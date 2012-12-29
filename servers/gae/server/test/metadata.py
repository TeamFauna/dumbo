import urllib
import urllib2

url = 'http://localhost:8080/getMetadata'
values = {'movieId': 0, 'timestamp': 0}

data = urllib.urlencode(values)
req = urllib2.Request(url,data)
response = urllib2.urlopen(req)
the_page = response.read()

print the_page
