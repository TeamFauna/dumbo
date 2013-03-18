import re

class IMDBParser:
  def __init__(self, castFilePath):
    self.actorInfo = {}
    self.castFilePath = castFilePath

  def getActorInfo(self):
    html = open(self.castFilePath, 'r').read()
    rows = re.findall('<tr class=".+?">.+?</tr>', html)
    for row in rows:
      actorName = re.findall('<td class="nm">.*?([\w\s\-\.\'()]+)<', row)[0].strip()
      characterName = re.findall('<td class="char">.*?([\w\s\-\.\'()]+)<', row)[0].strip()

      characterURL = re.findall('<td class="char">.*?<a href="(.+?)"', row)
      characterURL = characterURL[0] if characterURL else None

      actorURL = re.findall('<td class="nm">.*?<a href="(.+?)"', row)
      actorURL = actorURL[0] if actorURL else None

      picURL = re.findall('<td class="hs">.*?src="(.+?)"', row)
      picURL = picURL[0] if picURL else None

      self.actorInfo[actorName] = {
        'role': characterName,
        'actorURL': actorURL,
        'characterURL': characterURL,
        'picURL': picURL
      }
    return self.actorInfo

  def getName_(self, link):
    return link[link[:-5].rfind('>') + 1 : -4]

  def getURL_(self, link):
    return re.findall('/name/[a-z0-9]+/|/character/[a-z0-9]+/', link)[0]
