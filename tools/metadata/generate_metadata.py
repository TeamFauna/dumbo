"""
Generates movie metadata in the following format:

{
  imdb_url: "",
  name: "",
  roles: [
    { name: "", actor: 0, imdb_url: ""}
  ],
  actors: [
    { name: "", imdb_url: "", pic_url: "" }
  ],
  role_events: [
    { blurb: "", role: 0, timestamp: 324234 }
  ],
  plot_events: [
    { plot: "", timestamp: 0 }
  ]
}
"""
import json
import re
from line_parser import LineParser
from line_timestamper import LineTimestamper
from imdb_parser import IMDBParser

DEBUG = False;

def generateMetadata(subtitlePath, transcriptPath, imdbInfo):

  def getActors():
    actors = []
    for actor in actorInfo.keys():
      info = actorInfo[actor]
      actors.append({ "name": actor, "imdb_url": info['actorURL'], "pic_url": info['picURL'] })
    return actors

  def getRoleEvents():
    roleEvents = []
    for character in characterLines.keys():
      lines = characterLines[character]
      for line in lines:
        timestamp = lineTimestamper.timestamp(line)
        if timestamp < 0:
          if DEBUG: print 'ERROR', "couldn't find line \"%s\" said by %s" %(line, character)
        else:
          roleEvents.append({ "blurb": line, "role": getCharacterIndex_(character), "timestamp": timestamp })
    return roleEvents

  def getCharacterIndex_(character):
    for (actorIndex, actor) in enumerate(actorInfo.keys()):
      if looselyMatches_(actorInfo[actor]['role'], character):
        return actorIndex
    if DEBUG:
      print 'ERROR', 'character', character, 'not found on IMDB'
    return -1

  stopList = ['the', 'of', 'mr', 'ms', 'mrs', 'in', 'on']
  def looselyMatches_(words1, words2):
    words1 = stripPunct_(words1.lower())
    words2 = stripPunct_(words2.lower())
    if words1 is words2: return True
    words = set(words1.split(' '))
    for word in words2.split(' '):
      if word in words and not (word in stopList): return True
    return False

  def stripPunct_(word):
    return re.sub("[().,/\-']", "", word)

  def getRoles():
    characters = []
    for (actorIndex, actor) in enumerate(actorInfo.keys()):
      info = actorInfo[actor]
      characters.append({ "name": info['role'], "actor": actorIndex, "imdb_url": info['characterURL'] })
    return characters

  def getPlotEvents():
    return []

  def printJson():
    print json.dumps({
        "imdb_url": "URL",
        "name": 'NAME',
        "roles": getRoles(),
        "actors": getActors(),
        "role_events": getRoleEvents(),
        "plot_events": getPlotEvents()
    }, ensure_ascii=False)

  characterLines = LineParser(transcriptPath, subtitlePath).getLines()
  lineTimestamper = LineTimestamper(subtitlePath)
  actorInfo = IMDBParser(imdbInfo).getActorInfo()

  printJson()


if __name__ == "__main__":
  lotrSubs = 'data/TLOTR.The.Fellowship.of.the.Ring.2001.Extended.BluRay.1080p.DTSES6.1.2Audio.x264-CHD.srt'
  lotrTranscript = 'data/LOTR_1ex_transcript.txt'
  lotrCastFile = 'data/LOTR_1ex_cast.html'

  himymSubs = 'data/How I Met Your Mother - 6x10 - Blitzgiving.HDTV.LOL.en.srt'
  himymTranscript = 'data/HIMYM_S6E10_transcript.txt'
  himymCastFile = 'data/HIMYM_S6E10_cast.txt'

  #generateMetadata(lotrSubs, lotrTranscript, lotrCastFile)
  generateMetadata(himymSubs, himymTranscript, himymCastFile)
