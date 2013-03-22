"""
Generates movie metadata in the following format:
{
  imdb_url: "",
  name: "",
  roles: [
    { name: "", actor: 0, imdb_url: ""}
  ],
  actors: [
    { name: "", imdb_url: "", picture_url: "" }
  ],
  role_events: [
    { blurb: "", role: 0, time_stamp: 324234 }
  ],
  plot_events: [
    { plot: "", time_stamp: 0 }
  ]
}
"""
import json
import re
import imp
from line_parser import LineParser
from line_timestamper import LineTimestamper
from imdb_parser import IMDBParser

DEBUG = False;

def generateMetadata(path):

  def getActors():
    actors = []
    for actor in actorInfo.keys():
      info = actorInfo[actor]
      actors.append({ "name": actor, "imdb_url": info['actorURL'],
                      "picture_url": info['picURL'], 'bio': info['bio'] })
    return actors

  def getRoleEvents():
    roleEvents = []
    for character in characterLines.keys():
      lineTimestamper.reset()
      lines = characterLines[character]
      for line in lines:
        timestamp = lineTimestamper.timestamp(line)
        if timestamp < 0:
          if DEBUG: print 'ERROR', "couldn't find line \"%s\" said by %s" %(line, character)
        else:
          roleEvents.append({ "blurb": line, "role": getCharacterIndex_(character), "time_stamp": timestamp })
    return roleEvents

  def getCharacterIndex_(character):
    for (actorIndex, actor) in enumerate(actorInfo.keys()):
      if looselyMatches_(actorInfo[actor]['role'], character):
        return actorIndex
    if DEBUG: print 'ERROR', 'character', character, 'not found on IMDB'
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
    plotEvents = []
    for trivia in manual.trivia:
      lineTimestamper.reset()
      plotEvents.append({
          'time_stamp': lineTimestamper.timestamp(trivia['line']),
          'plot': trivia['trivia']
      })
    return plotEvents

  def printJson():
    # this will crash if the string contains any non-ascii characters
    print json.dumps({
        "imdb_url": manual.url,
        "name": manual.name,
        "summary": manual.summary,
        "roles": getRoles(),
        "actors": getActors(),
        "role_events": getRoleEvents(),
        "plot_events": getPlotEvents()
    }, ensure_ascii=False)

  manual = imp.load_source('manual', './data/' + path + '/manual.py')
  subtitlePath = 'data/' + path + '/subs.srt'
  transcriptPath = 'data/' + path + '/transcript.txt'
  imdbInfo = 'data/' + path + '/cast.html'

  characterLines = LineParser(transcriptPath, path).getLines()
  lineTimestamper = LineTimestamper(subtitlePath)
  actorInfo = IMDBParser(imdbInfo).getActorInfo()

  printJson()


if __name__ == "__main__":
  generateMetadata('futurama_s1e9')
  #generateMetadata('himym_s6e10')
