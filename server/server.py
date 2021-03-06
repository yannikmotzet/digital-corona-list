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
MAX_STORAGE_DAYS = 28
DATABASE_LIST_PATH = "storage.csv"
DATABASE_ROOMS_PATH = './rooms.json'
DATABASE_EVENTS_PATH = './events.json'


app = flask.Flask(__name__)
# app.config["DEBUG"] = True

# delete old data
today = date.today()
csv_df = pd.read_csv(DATABASE_LIST_PATH, parse_dates=False)
for index, row in csv_df.iterrows():
    difference_days = (today - datetime.strptime(row['date'], "%Y-%m-%d").date()).days
    if difference_days >= MAX_STORAGE_DAYS:
        csv_df.drop(index, inplace=True)
csv_df.to_csv(DATABASE_LIST_PATH, index=False)


# get rooms list
def load_rooms():
    with open(DATABASE_ROOMS_PATH) as json_file_rooms:
        rooms_json = json.load(json_file_rooms)
        return rooms_json


# get events list
def load_events():
    with open(DATABASE_EVENTS_PATH) as json_file_events:
        events_json = json.load(json_file_events)
    events_df = pd.read_json(DATABASE_EVENTS_PATH, convert_dates=False)
    return events_json, events_df


# html home page
@app.route('/', methods=['GET'])
def home():
    return '''<h1>digital corona list</h1>
<p>automatic digital corona list for public places using BLE beacons</p>'''


# http get for rooms
@app.route('/rooms', methods=['GET'])
def return_rooms():
    rooms_json = load_rooms()
    return jsonify(rooms_json)


# http get for events
@app.route('/events', methods=['GET'])
def return_events():
    __, events_json = load_events()
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
    e_mail = client_data_json['e_mail']

    # find corresponding lecture
    __, events_df = load_events()
    for __, row in events_df.iterrows():
        if row['room'] == room and row['date'] == date:
            if datetime.strptime(row['start_time'], "%H:%M") <= datetime.strptime(time, "%H:%M") and datetime.strptime(row['end_time'], "%H:%M") >= datetime.strptime(time, "%H:%M"):
                event_start_time = row['start_time']
                event_end_time = row['end_time']
                event_name = row['name']

                # check if already stored
                csv_df = pd.read_csv(DATABASE_LIST_PATH, parse_dates=False)
                for __, csv_row in csv_df.iterrows():
                    if (csv_row['room'] == room) and (csv_row['date'] == date):
                        if (csv_row['start_time'] == event_start_time) and (csv_row['name'] == event_name):
                            if(csv_row['given_name'] == given_name) and(csv_row['sur_name'] == sur_name):
                                return jsonify({'answer': "ERROR: data already saved", 'error': 1})
    
                # store data in csv
                data = room + "," + date + "," + event_start_time +  "," +  event_end_time +  "," +  event_name +  "," + time + "," + given_name + "," + sur_name + "," + phone + "," + e_mail +'\n'
                with open(DATABASE_LIST_PATH,'a') as fd:
                    fd.write(data)
                return jsonify({'answer':{'room': room, 'event_name': event_name}, 'error': 0})

    return jsonify({'answer': "ERROR: no event found", 'error': 1})


# http get for corona case, returns all entries of corresponding event
@app.route('/participants', methods=['GET'])
def return_participants():
    client_data_json = request.get_json()
    room = client_data_json['room']
    date = client_data_json['date']
    start_time = client_data_json['start_time']
    # check csv
    csv_df = pd.read_csv(DATABASE_LIST_PATH, parse_dates=False)
    for index, row in csv_df.iterrows():
        if (row['room'] == room) and (row['date'] == date) and (row['start_time'] == start_time):
            continue
        else: csv_df.drop(index, inplace=True)
    return csv_df.to_json()


# run server
local_ip = socket.gethostbyname(socket.gethostname())
app.run(host=local_ip)