import requests
import pandas as pd

ip_addr = 'http://192.168.1.115' +':5000'

data_store_1 = {'room': 'htwg-f123',
     'date': '2020-04-01',
     'time': '14:05',
     'given_name': 'Max',
     'sur_name': 'Mustermann',
     'phone': '',
     'e-mail': 'max.mustermann@mustermail.com'}

data_store_2 = {'room': 'htwg-f123',
     'date': '2020-04-01',
     'time': '14:06',
     'given_name': 'Philip',
     'sur_name': 'Mustermann',
     'phone': '',
     'e-mail': 'philip.mustermann@mustermail.com'}

data_participants_request = {'room': 'htwg-f123',
     'date': '2020-04-01',
     'start_time': '14:00'}

# store data
url = ip_addr + '/store'
r = requests.post(url, json=data_store_1)
print(r.text)

url = ip_addr + '/store'
r = requests.post(url, json=data_store_2)
print(r.text)

# get rooms
url = 'http://192.168.1.115:5000/rooms?pw=12345!'
r = requests.get(url)
rooms_df = pd.read_json(r.text)
print(rooms_df)

# get participants
url = ip_addr + '/participants'
r = requests.get(url, json=data_participants_request)
participants_df = pd.read_json(r.text)
print(participants_df)