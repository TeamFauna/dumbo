"""
Given a transcript file, iterates through the first line each character said.
"""

import re

class LineParser:
  def __init__(self, filePath, type):
    self.transcript = open(filePath, 'r')
    self.characterLines = {}
    if type.find('LOTR') > -1: self.type = 'LOTR'
    else: self.type = 'other'

  def getLines(self):
    for line in self.transcript:
      line.rstrip()
      if self.type is 'LOTR':
        character, line = self.parseLine1_(line)
      else:
        character, line = self.parseLine2_(line)
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
    if len(line) is 0:
      return (None, None)
    colonIndex = line.find(':')
    if colonIndex < 0:
      return (None, None)
    actor = line[:colonIndex]
    phrase = line[colonIndex+1:].strip()
    phrase = self.parseNonText_(phrase)
    actor = self.parseNonText_(actor)
    return (actor, phrase)

  def parseNonText_(self, phrase):
    phrase = re.sub("<.+?>", "", phrase).strip()
    phrase = re.sub("\(.+?\)", "", phrase).strip()
    if phrase.find('>>') > -1:
      phrase = re.sub(">>.*", "", phrase).strip()[:-2]
    return phrase
