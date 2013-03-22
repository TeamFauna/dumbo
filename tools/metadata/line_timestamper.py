"""
Given a phrase, returns the time in milliseconds it was said.
"""

import math
import re

DEBUG = False

class LineTimestamper:
  def __init__(self, subtitlesPath):
    self.subs = open(subtitlesPath, 'r').read().lower()
    self.reset()
    self.matchers = [self.exactMatch_, self.noEndingPunctMatch_, self.noPunctMatch_, self.charOnlyMatch_]

  def reset(self):
    self.lastTimestamp = 0

  def timestamp(self, phrase):
    results = []
    for (matchIndex, confidence, match) in self.findPotentialMatches_(phrase.lower()):
      if matchIndex > 0:
        result = self.getTimestampForIndex_(matchIndex)
        if result < self.lastTimestamp:
          results.append((result, confidence))
        else:
          self.lastTimestamp = result
          return int(result/1000)
    bestResult = -1
    bestConfidence = 0
    results.reverse()
    for (result, confidence) in results:
      confidence += 3 if result > self.lastTimestamp else 0
      if confidence > bestConfidence:
        bestConfidence = confidence
        bestResult = result
    return int(bestResult/1000)

  def findPotentialMatches_(self, fullPhrase):
    for phrase in self.getSubPhrases_(fullPhrase):
      for matcher in self.matchers:
        for match in matcher(phrase):
          yield match

  def getSubPhrases_(self, phrase):
    yield phrase
    for sentence in self.splitOn_(phrase, '[.!?]'): yield sentence
    for sentence in self.splitOn_(phrase, '[.!?,;:]'): yield sentence
    for sentence in self.splitOn_(phrase, '[.!?,;:\-]'): yield sentence
    for length in [20, 15, 10]:
      start = 0
      while start + length < len(phrase):
        yield phrase[start:start + length].strip()
        start += length
      if length + 4 < len(phrase):
        yield phrase[:-length]

  def splitOn_(self, phrase, regex):
    sentences = [sentence.strip() for sentence in re.split(regex, phrase)]
    if len(sentences) is 1:
      return
    sentences.sort(key=lambda sentence: (-len(sentence), sentence))
    for sentence in sentences:
      if len(sentence) > 4: yield sentence

  def exactMatch_(self, phrase):
    for result in self.getMatchedSubbedPhrases_(phrase, "}"):
      yield (result.start(), len(phrase), phrase)

  def noEndingPunctMatch_(self, phrase):
    for result in self.getMatchedSubbedPhrases_(phrase, "[.?!]"):
      yield (result.start(), len(phrase) * .97, phrase)

  def noPunctMatch_(self, phrase):
    for result in self.getMatchedSubbedPhrases_(phrase, "[.,?!]"):
      yield (result.start(), len(phrase) * .94, phrase)

  def charOnlyMatch_(self, phrase):
    for result in self.getMatchedSubbedPhrases_(phrase, "[;:'\".,?!]"):
      yield (result.start(), len(phrase) * .91, phrase)

  def getMatchedSubbedPhrases_(self, phrase, sub):
    for result in re.finditer(re.escape(re.sub(sub, "", phrase)), self.subs):
      yield result

  def getTimestampForIndex_(self, index):
    timeEnd = self.subs.rfind(' --> ', 0, index)
    time = self.subs[timeEnd-12:timeEnd]
    try:
      return self.getMilliseconds_(int(time[0:2]), int(time[3:5]), int(time[6:8]), int(time[9:12]))
    except:
      if DEBUG: print 'ERROR', "\"%s\" should be a timestamp" % (time)
      return -1

  def getMilliseconds_(self, hours, mins, secs, millisecs):
    return ((hours * 60 + mins) * 60 + secs) * 1000 + millisecs
