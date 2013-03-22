import HTMLParser
import re
import urllib2

htmlParser = HTMLParser.HTMLParser()

class IMDBParser:
  def __init__(self, castFilePath):
    self.actorInfo = {}
    self.castFilePath = castFilePath

  def getActorInfo(self):
    html = open(self.castFilePath, 'r').read()
    rows = re.findall('<tr class=".+?">.+?</tr>', html)
    for row in rows:
      actorName = re.search('<td class="nm">.*?([\w\s\-\.\'()]+)<', row).group(1).strip()
      characterName = re.search('<td class="char">.*?([\w\s\-\.\'()]+)<', row).group(1).strip()

      characterURL = re.search('<td class="char">.*?<a href="(.+?)"', row)
      characterURL = 'http://www.imdb.com' + characterURL.group(1) if characterURL else None

      actorURL = re.search('<td class="nm">.*?<a href="(.+?)"', row)
      actorURL = 'http://www.imdb.com' + actorURL.group(1) if actorURL else None

      picURL = re.findall('<td class="hs">.*?src="(.+?)"', row)
      if picURL != None:
        picURL = picURL[0].replace('SX23_SY30_.jpg', 'SX214_CR0,0,214,314_.jpg')
      (bio, picURL) = self.getDetailedActorInfo_(actorURL) if actorURL else None

      self.actorInfo[actorName] = {
        'role': characterName,
        'bio': bio,
        'actorURL': actorURL,
        'characterURL': characterURL,
        'picURL': picURL
      }
    return self.actorInfo

  def getDetailedActorInfo_(self, actorURL):
    request = urllib2.urlopen(actorURL)
    page = request.read()
    results = (self.getBio_(page), self.getPic_(page))
    return results

  def getBio_(self, page):
    bio = re.search('<span itemprop="description">(.+?)<', page, re.DOTALL)
    if bio is None:
      result = self.getMovieAppearances_(page)
    else:
      bio = bio.group(1)
      if bio[-3:] != '...':
        bio = bio.strip() + '...'
      result = bio
    return htmlParser.unescape(result)

  def getPic_(self, page):
    return re.search('id="img_primary".*?<img\s+src="(.*?)"', page, re.DOTALL).group(1)

  def getMovieAppearances_(self, page):
    movies = re.findall('<div\s*class="filmo-row.*?<a\s*onclick=.*?>\s*(.*?)\s*</a>', page, re.DOTALL)
    if len(movies) is 0:
      return ''
    movies = movies[:20] if len(movies) > 20 else movies
    return ', '.join(movies)

  def getName_(self, link):
    return link[link[:-5].rfind('>') + 1 : -4]

  def getURL_(self, link):
    return 'http://www.imdb.com' + re.search('/name/[a-z0-9]+/|/character/[a-z0-9]+/', link).group(0)
