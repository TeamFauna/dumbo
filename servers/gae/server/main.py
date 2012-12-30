#!/usr/bin/env python

import webapp2

from src.MainHandler import MainHandler
from src.RecognizeMovieHandler import RecognizeMovieHandler
from src.MetadataHandler import MetadataHandler

handlers = [
    ('/', MainHandler),
    ('/recognizeMovie', RecognizeMovieHandler),
    ('/getMetadata', MetadataHandler)]

app = webapp2.WSGIApplication(handlers, debug=True)

