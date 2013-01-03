from google.appengine.ext import db
from Fingerprint import Fingerprint

"""
This model represents a movieId-timestamp pair, and links to their
Fingerprint
"""
class MovieTimePair(db.Model):
  movieId = db.StringProperty(required=True)
  timestamp = db.IntegerProperty(required=True)
  fingerprint = db.ReferenceProperty(Fingerprint)

