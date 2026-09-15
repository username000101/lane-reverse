# Frida cкрипт для дампа /proc/self/maps

import frida
import sys

def on_message(message, data):
    if message['type'] == 'send':
        print(message['payload'])
    elif message['type'] == 'error':
        print(message['stack'])

device = frida.get_usb_device(timeout=5)

package = "com.skiy.lane"

pid = device.spawn([package])
session = device.attach(pid)

script_code = """
try {
    var maps = File.readAllText("/proc/self/maps");
    send(maps);
} catch (e) {
    send("error: " + e.message);
}
"""

script = session.create_script(script_code)
script.on('message', on_message)
script.load()

device.resume(pid)

sys.stdin.read()  # держим процесс живым, чтобы получить сообщение