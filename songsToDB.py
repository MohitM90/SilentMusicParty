#!/usr/bin/python
# -*- coding: utf_8 -*-

import codecs


import sqlite3
from random import randint


conn = sqlite3.connect('SongsAndPlaylists.db')
c = conn.cursor()
print "Opened database successfully";

pathprefix = "http://35.157.214.129:56183/songsIPTK/"



c.execute('''CREATE TABLE "Songs" ( `song_id` INTEGER PRIMARY KEY AUTOINCREMENT UNIQUE, `title` TEXT UNIQUE, `album` TEXT, `artist` TEXT, `path` TEXT );''')
c.execute('''CREATE TABLE "Playlists" ( `playlist_id` INTEGER PRIMARY KEY AUTOINCREMENT UNIQUE, `title` TEXT);''')
c.execute('''CREATE TABLE `Playlists_Songs` ( `playlist_id` INTEGER NOT NULL, `song_id` INTEGER NOT NULL );''')



#file = open("top100_utf8.txt", "r")
file = codecs.open('top100.txt', encoding='cp1252')

for i, line in enumerate(file):
	if i == 6:
		continue
	#line = line.decode("utf_8").encode("ascii")
	line = line.strip()
	pathline = line
	if line[0] == "0":
		pathline = line[1:]
	pathline = pathline.strip("?")
	path = pathprefix + pathline + ".mp3"
	line = line[5:]
	artist_title = line.split(" - ")
	artist = artist_title[0]
	title = artist_title[1]
	print i , "."
	print "Path: " + path
	print "Artist = ", artist
	print "Title = ", title
	c.execute("INSERT INTO Songs (title, artist, path) VALUES (?,?,?);", (title, artist, path))
		


		
c.execute('''INSERT INTO Playlists (title) VALUES ("Meine Lieblingssongs");''')
for i in range(9):
	c.execute("INSERT INTO Playlists_Songs (playlist_id, song_id) VALUES (1,?);", (randint(1,99),))

c.execute('''INSERT INTO Playlists (title) VALUES ("TOP 10");''')
for i in range(1,11):
	c.execute("INSERT INTO Playlists_Songs (playlist_id, song_id) VALUES (2,?);", (i,))
	
c.execute('''INSERT INTO Playlists (title) VALUES ("Random");''')
for i in range(15):
	c.execute("INSERT INTO Playlists_Songs (playlist_id, song_id) VALUES (3,?);", (randint(1,99),))



conn.commit()
conn.close()
