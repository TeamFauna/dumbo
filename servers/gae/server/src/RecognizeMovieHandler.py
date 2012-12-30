import json
import webapp2
 
class RecognizeMovieHandler(webapp2.RequestHandler):
  def get(self):
    self.response.headers['Content-Type'] = 'text/json'

    hashes = self._parsehashes(self.request.get('hashes'))

    movieId = self.request.get('movieId')
    timestamp = 0

    if movieId:
      movieId = int(movieId)
      timestamp = self._findtime(movieId, hashes)
    else:
      movieId, timestamp = self._findmovie(hashes)

    data = { 'movieId': movieId, 'timestamp': timestamp }
    output = json.dumps(data)

    self.response.out.write(output)
    
  # Takes in a string representing a comma-delimited list of 32-byte integers
  # Retures a list of 32-byte integers
  def _parsehashes(self, hashes_string):
    hashes = hashes_string.split(',')
    return map(int, hashes)

  def _findtime(self, movieId, hashes):
    # TODO: Implement
    return 10

  def _findmovie(self, hashes):
    # TODO: Implement
    return 0, 20

