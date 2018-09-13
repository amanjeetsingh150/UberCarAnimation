import json
import paho.mqtt.client as mqtt
import random
import time
import threading
import sys
import requests
import json

import sys
sourceArray = sys.argv
source=""

#Your API KEY HERE
api_key=" " 
for x in sys.argv:
    place=x+'+'
    source+=place
api_url="https://maps.googleapis.com/maps/api/directions/json?mode=driving&origin=28.671246,77.317654&destination="+str(source)+"&transit_routing_preference=less_driving&key="+str(api_key)
 
def decode_polyline(polyline_str):
    index, lat, lng = 0, 0, 0
    coordinates = []
    changes = {'latitude': 0, 'longitude': 0}

    # Coordinates have variable length when encoded, so just keep
    # track of whether we've hit the end of the string. In each
    # while loop iteration, a single coordinate is decoded.
    while index < len(polyline_str):
        # Gather lat/lon changes, store them in a dictionary to apply them later
        for unit in ['latitude', 'longitude']: 
            shift, result = 0, 0

            while True:
                byte = ord(polyline_str[index]) - 63
                index+=1
                result |= (byte & 0x1f) << shift
                shift += 5
                if not byte >= 0x20:
                    break

            if (result & 1):
                changes[unit] = ~(result >> 1)
            else:
                changes[unit] = (result >> 1)

        lat += changes['latitude']
        lng += changes['longitude']

        coordinates.append((lat / 100000.0, lng / 100000.0))

    return coordinates

mqttc=mqtt.Client("DriverClient",clean_session=False)
#YOUR MQTT SERVER USERNAME PASSWORD HERE
mqttc.username_pw_set("username","password")
#YOUR MQTT SERVER URI AND PORT HERE
mqttc.connect("SERVER URI","PORT",60)
    
   
def getSteps():
    res = requests.get(api_url)
    result=res.json()  
    List = result["routes"]
    for route in List:
        polyline = route['overview_polyline']['points']
        latLngList = decode_polyline(polyline)
    for latLng in latLngList:
        s=str(latLng)
        b = bytearray()
        b.extend(s) 
 	print latLng
        mqttc.publish("location/track", b, qos=0)
        time.sleep(1)
    return


getSteps()
