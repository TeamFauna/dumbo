import json
import webapp2

class MetadataHandler(webapp2.RequestHandler):
  def get(self):
    self.response.headers['Content-Type'] = 'text/json'

    movieId = self.request.get('movieId')
    timestamp = self.request.get('timestamp')
    metadata = self._getmetadata(movieId, timestamp)

    data = { 'metadata': metadata }
    output = json.dumps(data)
    self.response.out.write(output)

  def _getmetadata(self, movieId, timestamp):
    # TODO: implement
    return [{ 'type': 'text', 'title': 'Awesome Quote', 'description': 'Noah: I like poop <3' },
            { 'type': 'link', 'title': 'Relevent Data', 'description': 'Om noms noms', 'url': 'http://google.com' }]

