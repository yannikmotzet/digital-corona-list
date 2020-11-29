import requests

data = {'room': 'htwg-f123',
     'date': '2020-04-01',
     'time': '14:05',
     'given_name': 'Max',
     'sur_name': 'Mustermann',
     'phone': '',
     'e-mail': 'max.mustermann@mustermail.com'}

url = 'http://192.168.1.115:5000/store'
r = requests.post(url, json=data)
print(r.text)

# url = 'http://192.168.1.115:5000/rooms?pw=12345!'
# r = requests.get(url)
# print(r.text)