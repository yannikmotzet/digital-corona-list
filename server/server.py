import flask
from flask import request, jsonify
import socket
import json
import pandas as pd
from datetime import datetime

passphrase="12345!"

app = flask.Flask(__name__)
# app.config["DEBUG"] = True

# rooms database
with open('./rooms.json') as json_file_rooms:
    rooms_json = json.load(json_file_rooms)

# events database
with open('./events.json') as json_file_events:
    events_json = json.load(json_file_events)
events_df = pd.read_json('./events.json', convert_dates=False)
print(type(events_df))


@app.route('/', methods=['GET'])
def home():
    return '''<h1>digital corona list</h1>
<p>automatic digital corona list for public places using BLE beacons</p>'''

# http://192.168.1.115:5000/rooms?pw=12345!
@app.route('/rooms', methods=['GET'])
def return_rooms():
    if 'pw' in request.args and request.args['pw'] == passphrase:
        return jsonify(rooms_json)
    else:
        return '''access denied'''

@app.route('/events', methods=['GET'])
def return_events():
    return jsonify(events_json)

# TODO store data
@app.route('/store', methods=['POST'])
def store_user_data():
    client_data_json = request.get_json()
    room = client_data_json['room']
    date = client_data_json['date']
    time = client_data_json['time']
    given_name = client_data_json['given_name']
    sur_name = client_data_json['sur_name']
    phone = client_data_json['phone']
    e_mail = client_data_json['e-mail']

    # find corresponding lecture
    for __, row in events_df.iterrows():
        if row['room'] == room and row['date'] == date:
            if datetime.strptime(row['start_time'], "%H:%M") <= datetime.strptime(time, "%H:%M") and datetime.strptime(row['end_time'], "%H:%M") >= datetime.strptime(time, "%H:%M"):
                event_start_time = row['start_time']
                event_end_time = row['end_time']
                event_name = row['name']
    
                # store data in csv
                data = room + "," + date + "," + event_start_time +  "," +  event_end_time +  "," +  event_name +  "," + time + "," + given_name + "," + sur_name + "," + phone + "," + e_mail +'\n'
                with open('storage.csv','a') as fd:
                    fd.write(data)
                return room + ',' + event_name

    return 'ERROR: no lecture found'


# run server
local_ip = socket.gethostbyname(socket.gethostname())
app.run(host=local_ip)