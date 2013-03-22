"""
Given a transcript file, iterates through the first line each character said.
"""

import re

class LineParser:

  def __init__(self, filePath, type):
    self.transcript = open(filePath, 'r')
    self.characterLines = {}
    self.type = type

  def getLines(self):
    for line in self.transcript:
      line.rstrip()
      if self.type.find('lotr') > -1:
        character, line = self.parseLine1_(line)
      elif self.type.find('himym') > -1:
        character, line = self.parseLine2_(line)
      else:
        character, line = self.parseLine3_(line)
      if character is None: continue
      if not(character in self.characterLines):
        self.characterLines[character] = []
      self.characterLines[character].append(line)
    return self.characterLines

  def parseLine1_(self, line):
    if not line.startswith("<<"):
      return (None, None)
    colonIndex = line.find(':')
    if colonIndex < 0:
      return (None, None)
    actor = line[2:colonIndex]
    phrase = line[colonIndex+2:-3]
    phrase = self.parseNonText_(phrase)
    actor = self.parseNonText_(actor)
    return (actor, phrase)

  def parseLine2_(self, line):
    if len(line) is 0 or line[0] is '[':
      return (None, None)
    colonIndex = line.find(':')
    if colonIndex < 0:
      return (None, None)
    actor = line[:colonIndex]
    phrase = line[colonIndex+1:].strip()
    phrase = self.parseNonText_(phrase)
    actor = self.parseNonText_(actor)
    return (actor, phrase)

  def parseLine3_(self, line):
    line = line.strip()
    if line is '$END$':
      return self.getCurrentActorLine_()
    if len(line) is 0 or line[0] is '[':
      return self.getCurrentActorLine_()

    colonIndex = line.find(':')
    if colonIndex < 0:
      self.currentPhrase += ' ' + line
      return (None, None)

    result = self.getCurrentActorLine_()
    self.currentActor = self.parseNonText_(line[:colonIndex])
    self.currentPhrase = self.parseNonText_(line[colonIndex+1:])
    return result

  def getCurrentActorLine_(self):
    if not hasattr(self, 'currentActor'):
      self.currentActor = self.currentPhrase = None
    if self.currentActor:
      result = (self.currentActor, self.currentPhrase)
      self.currentActor = self.currentPhrase = None
      return result
    return (None, None)

  def parseNonText_(self, phrase):
    phrase = re.sub("<.+?>", "", phrase).strip()
    phrase = re.sub("\(.+?\)", "", phrase).strip()
    phrase = re.sub("\[.+?\]", "", phrase).strip()
    if phrase.find('>>') > -1:
      phrase = re.sub(">>.*", "", phrase).strip()[:-2]
    return phrase
