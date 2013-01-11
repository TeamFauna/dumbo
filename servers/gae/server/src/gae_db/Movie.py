from google.appengine.ext import db

"""
The main movie Database Expando, all the properties that go with a movie
should go here

With the Expando type you can dynamically add properties to the Movie 
instances
https://developers.google.com/appengine/docs/python/datastore/datamodeling#The_Expando_Class
"""
class Movie(db.Expando):
  movieId = db.StringProperty(required=True) #Might replace this with the default key created by GAE
  name = db.StringProperty(required=True)
  length = db.IntegerProperty(required=True)
