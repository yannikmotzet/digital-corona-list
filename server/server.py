import flask
from flask import request, jsonify
import socket
import json
import pandas as pd
from datetime import datetime, date
import csv

__author__ = 'Yannik Motzet'
__email__ = 'yannik.motzet@outlook.com'

PASSPHRASE="12345!"
MAX_STORAGE_DAYS = 21
DATABASE_LIST_PATH = "storage.csv"
DATABASE_ROOMS_PATH = './rooms.json'
DATABASE_EVENTS_PATH = './rooms.json'


app = flask.Flask(__name__)
# app.config["DEBUG"] = True

# delete old data
today = date.today()
csv_df = pd.read_csv(DATABASE_LIST_PATH, parse_dates=False)
print(csv_df)
for index, row in csv_df.iterrows():
    difference_days = (today - datetime.strptime(row['date'], "%Y-%m-%d").date()).days
    if difference_days >= MAX_STORAGE_DAYS:
        csv_df.drop(index, inplace=True)
csv_df.to_csv(DATABASE_LIST_PATH, index=False)


# rooms database
with open(DATABASE_ROOMS_PATH) as json_file_rooms:
    rooms_json = json.load(json_file_rooms)


# events database
with open(DATABASE_EVENTS_PATH) as json_file_events:
    events_json = json.load(json_file_events)
events_df = pd.read_json(DATABASE_EVENTS_PATH, convert_dates=False)
print(type(events_df))


# html home page
@app.route('/', methods=['GET'])
def home():
    return '''<h1>digital corona list</h1>
<p>automatic digital corona list for public places using BLE beacons</p>'''


# http get for rooms
@app.route('/rooms', methods=['GET'])
def return_rooms():
    if 'pw' in request.args and request.args['pw'] == PASSPHRASE:
        return jsonify(rooms_json)
    else:
        return '''access denied'''


# http get for events
@app.route('/events', methods=['GET'])
def return_events():
    return jsonify(events_json)


# http post for storing data
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
                with open(DATABASE_LIST_PATH,'a') as fd:
                    fd.write(data)
                return room + ',' + event_name

    return 'ERROR: no lecture found'


# run server
local_ip = socket.gethostbyname(socket.gethostname())
app.run(host=local_ip)