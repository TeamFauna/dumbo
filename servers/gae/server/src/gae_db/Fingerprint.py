from google.appengine.ext import db

"""
This model represents the incredible amount of hashes that we will
create for every single 10ms segment of every clip.  I don't think GAE
is going to scale the way we want here, BUT LET'S FIND OUT!!!
"""
class Fingerprint(db.Model):
  """
  Oddly enough, we don't need anything here,
  
  The actual hash value of the fingerprint should be put in the
  default "key_name" attribute.

  And the movieId - timestamp pairs are created in the MovieTimePair class
  and can be referenced using the attribute "movietimepair_set"
  """
