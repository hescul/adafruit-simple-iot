import paho.mqtt.client as mqtt
import time, queue, serial, threading
from serial.serialutil import SerialException
import serial.tools.list_ports as serialtool

# -------------------------- CONFIGURATIONS & CONSTANTS SETUP -----------------------
# Broker Configurations
HOST_NAME = "io.adafruit.com"   # using Adafruit IO server
HOST_PORT = 8883                # secure connection
USERNAME  = "***"
PASSWORD  = "***"

# Gateway Configurations
GATEWAY_ID  = "GTW002"
GROUP_KEY   = "iot-lab"
PUB_QOS = 1
SUB_QOS = 1
STAT_TOPIC  = "gateway-status"
SUBSCRIBE_TOPICS = [
    "arduino-led",      # record led status
    "arduino-servo",    # record change in servo angle
    STAT_TOPIC          # listen to disconnect signal from the broker
]
SCAN_DELAY  = 1.0
TIME_OUT    = 5.0       # maximum waiting time for blocking
MAX_FAILED_ATTEMPTS = 3

# Devices Configurations
BAUDRATE    = 9600    # Arduino default baudrate
COM_TOKEN   = 'USB Serial Device'


# ----------------------------------- COLOR CODES ------------------------------------
class col:
    cdtag   = '\033[35;1m'
    mestag  = '\033[1;34m'
    pubtag  = '\033[31;1m'
    subtag  = '\033[0;33m'
    good    = '\033[92m'
    bad     = '\033[31m'
    user    = '\033[0;95m'
    topic   = '\033[1;37m'
    message = '\033[0;94m'
    stage   = '\033[0;93m'
    esc     = '\033[0m'


# --------------------------------- GLOBAL VARIABLES ---------------------------------
block       = False # flag to block thread
exquit      = False # flag to raise disconnect interrupt
greeting    = True  # flag to indicate whether this is the first time we connect
reinit      = False # flag to call init() procedure on reconnect
texceed     = False # flag to indicate we've reached maximum blocking time
failcount   = 0     # count attempts on fail activity
msg         = ''

# ---------------------------------- SHARED-MEMORY -----------------------------------
messageBuffer = queue.Queue()
devicesBuffer = queue.Queue()
terminate     = False
scanning      = False


# ------------------------------- CALLBACK DEFINITIONS -------------------------------
def on_connect(client, userdata, flags, rc):
    if rc != 0:
        print(f"{col.cdtag}[C]{col.esc} {GATEWAY_ID} failed to connect to {USERNAME}: {col.bad}{mqtt.connack_string(rc)}{col.esc}")
    else:
        global greeting
        if greeting:
            print(f"{col.cdtag}[C]{col.esc} {GATEWAY_ID} connected to {col.user}{USERNAME}{col.esc} with result: {col.good}{mqtt.connack_string(rc)}{col.esc}")
            greeting = False
            global block
            block = False
        else:
            print(f"{col.cdtag}[C]{col.esc} Reconnected to {col.user}{USERNAME}{col.esc} with result: {col.good}{mqtt.connack_string(rc)}{col.esc}")

def on_disconnect(client, userdata, rc):
    print(f"{col.cdtag}[D]{col.esc} {GATEWAY_ID} disconnected from {col.user}{USERNAME}{col.esc}")
    if rc != 0:
        print(f"{col.bad}[WARNING]{col.esc} Unexpected Disconnection --> {mqtt.error_string(rc)}")
        global failcount, reinit, scanning
        reinit = True
        scanning = False
        failcount = failcount + 1

# userdata holds a list of (string) in which: [0] -> payload; [1] -> topic's name
def on_message(client, userdata, message):
    msgv = message.payload.decode()                         # get message value
    msgt = message.topic[message.topic.find('.') + 1:]      # get message topic
    # filter out owning message
    if msgv == userdata[0] and msgt == userdata[1]:
        global block    # release block
        block = False
    else:
        print(f"{col.mestag}[M]{col.esc} Received {col.message}{msgv}{col.esc} on topic: {col.topic}{msgt}{col.esc}")
        # if receive offline instruct from the broker
        if (msgt == STAT_TOPIC and msgv == "offline"):
            global exquit
            exquit = True
        
        # otherwise add message to the buffer
        else:
            if scanning: messageBuffer.join()
            messageBuffer.put((msgt, msgv))

# userdata holds a list of (string) in which: [0] -> payload; [1] -> topic's name
def on_publish(client, userdata, mid):
    if "get" in userdata[1]:
        print(f"Fetching {col.stage}latest data{col.esc} from {userdata[1][:-4]}...")
    elif userdata[1] is STAT_TOPIC:
        print(f"{GATEWAY_ID} has set its status to {col.stage}{userdata[0]}{col.esc}")
    else:
        print(f"{col.pubtag}[P]{col.esc} Published {col.pubtag}{userdata[0]}{col.esc} to: {col.topic}{userdata[1]}{col.esc}")

# userdata is set to the subscribed topic's name (string)
def on_subscribe(client, userdata, mid, granted_qos):
    global block
    print(f"{col.subtag}[S]{col.esc} Subscribed to: {col.topic}{userdata}{col.esc}")
    block = False   # release block


# --------------------------------- UTILITY FUNCTIONS ---------------------------------
# call right after having (re)connected to the broker 
def init():
    global gtw, ser
    # subscribe to required topics
    # -----
    for top in SUBSCRIBE_TOPICS:
        global block
        block = True    # raise a block flag
        gtw.user_data_set(top)
        gtw.subscribe(topic=f"{USERNAME}/feeds/{GROUP_KEY}.{top}", qos=SUB_QOS)
        while block:
            pass        # block thread until fully subscribed
        # fetch latest data, except for the 'status' topic
        if top is not STAT_TOPIC:
            publish('', f"{top}/get")

    # publish 'online' to status feed
    # ------
    publish("online", STAT_TOPIC)

def publish(payload, topic):
    global gtw
    gtw.user_data_set([payload, topic])
    pub = gtw.publish(f"{USERNAME}/feeds/{GROUP_KEY}.{topic}", payload, PUB_QOS)
    pub.wait_for_publish()
    # if publishing to subscribed topic
    if topic in SUBSCRIBE_TOPICS:
        global block, texceed
        # then block thread until having fully filtered out the owning message
        block = True
        texceed = False
        taskTiming = threading.Thread(target=timer) # only block for the maximum time decided by the timer
        taskTiming.start()
        while block:
            if texceed: # timer exceeds
                print(f"{col.bad}[WARNING]{col.esc} Reached maximum waiting time for validating own message. Continuing...")
                taskTiming.join()
                block = False

def getport():
    ports = serialtool.comports()
    target = ''
    for p in ports:
        strport = str(p)
        if COM_TOKEN in strport:
            target = strport.split(" ")[0]
    if target == '':
        print(f"{col.bad}[WARNING]{col.esc} no COM port found")
    else:
        print(f"Attached to: {target}")
    return target

# data is in the form "!<num>:<topic>:<value>#"
def dispatch(data):
    data = data.replace("!", "")
    data = data.replace("#", "")
    sdata = data.split(":")
    if scanning: devicesBuffer.join()
    devicesBuffer.put((sdata[1], sdata[2]))

# this routine will be constantly called to read any serial data
def driver():
    bytesToRead = ser.inWaiting()
    if bytesToRead > 0:
        global msg
        msg = msg + ser.read(bytesToRead).decode("UTF-8")
        while ("#" in msg) and ("!" in msg):
            begin = msg.find("!")
            end = msg.find("#")
            dispatch(msg[begin : end + 1])
            if end == len(msg):
                msg = ''
            else:
                msg = msg[end + 1 :]

# the co-routine to scan for incoming serial data
def watcher():
    global exquit, ser
    while True:
        if terminate:
            ser.close()
            break
        try:
            driver()
        except SerialException:
            print(f"{col.bad}[WARNING]{col.esc} Lost connection to serial. Quitting...")
            ser.close()
            exquit = True
            break

# the co-routine to time the waiting
def timer():
    global texceed
    count = 0
    while True:
        time.sleep(1.0)
        count = count + 1
        if count == TIME_OUT:
            texceed = True
            break


# ------------------------------- MAIN GATEWAY SCRIPT --------------------------------

# instantiate a gateway client and setup some options
# -----
gtw = mqtt.Client(client_id=GATEWAY_ID)
gtw.tls_set_context()
gtw.username_pw_set(username=USERNAME, password=PASSWORD)
gtw.will_set(f"{USERNAME}/feeds/{GROUP_KEY}.{STAT_TOPIC}", 'offline')

# register callbacks
# -----
gtw.on_connect = on_connect
gtw.on_disconnect = on_disconnect
gtw.on_publish = on_publish
gtw.on_subscribe = on_subscribe
gtw.on_message = on_message

# connect to serial com port
# -----
try:
    print(f"{col.stage}Finding serial port...{col.esc}")
    ser = serial.Serial(getport(), BAUDRATE)
except SerialException:
    print("Quitting...")
    exit(1)

# start external thread
# -----
taskWatching = threading.Thread(target=watcher)
taskWatching.start()

# connect to the broker
# -----
gtw.connect(host=HOST_NAME, port=HOST_PORT)

# start recording change
# -----
gtw.loop_start()

# block thread until having connected successfully
# -----
block = True
failcount = 0
while block:
    if failcount == MAX_FAILED_ATTEMPTS:
        terminate = True
        taskWatching.join()
        gtw.disconnect()
        time.sleep(0.1)
        print("Maximum connection attempts tried. Quiting...")
        exit(1)

# subscribe to required topics & publish 'online' to status feed
# -----
init()

# monitor external devices
# -----
print(f"{col.stage}Monitoring...{col.esc}")
scanning = True
try:
    while True:
        # do the init procuder again on reconnect
        if gtw.is_connected and reinit:
            init()
            reinit = False
            scanning = True
        
        # if there still be data waiting to be published in the buffer
        while gtw.is_connected and not reinit and not devicesBuffer.empty():
            tup = devicesBuffer.get()
            publish(tup[1], tup[0])
            devicesBuffer.task_done()

        # if there still be message waiting to be processed in the buffer
        while not messageBuffer.empty():
            tup = messageBuffer.get()
            ser.write(f"!{SUBSCRIBE_TOPICS.index(tup[0])}:{tup[1]}#".encode())
            messageBuffer.task_done()

        # constantly check if there is a terminate signal from broker
        if exquit:
            raise KeyboardInterrupt


        # scan delay
        time.sleep(SCAN_DELAY)

# disconnect on cancel
# -----
except KeyboardInterrupt:
    # terminate watcher thread
    print("Terminating watcher...")
    terminate = True
    taskWatching.join(timeout=1.0)
    publish("offline", STAT_TOPIC)
    res = gtw.disconnect()
    time.sleep(0.1) # delay a small amount of time for on_disconnect() callback
    print(f"Disconnect Status: {col.good if res == 0 else col.bad}{mqtt.error_string(res)}{col.esc}")
