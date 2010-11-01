#!/bin/bash

# find . -name E2V3B1S88N28HF.2010-\* -print0 | xargs -0 zgrep .mp3 | awk '{print $10, $8}' > mp3month.txt

function foo {
  nTotal=`cat $1 | wc -l`
  echo  "Total accesses: $nTotal"
  
  nWhirled=`grep "^http://www.whirled.com" $1 | wc -l`
  echo  "Through Whirled URL: $nWhirled ($[100*$nWhirled/$nTotal]%)"
  
  nStub=`grep "^http://mediacloud.whirled.com/MediaStub.swf" $1 | wc -l`
  echo  "Through stub loader : $nStub ($[100*$nStub/$nTotal]%)"
  
  nNone=`grep "^- " $1 | wc -l`
  echo  "Without explicit referer : $nNone ($[100*$nNone/$nTotal]%)"
  
  nRemains="$[$nTotal-$nWhirled-$nStub-$nNone]"
  echo "Remaining miscellaneous origins : $nRemains ($[100*$nRemains/$nTotal]%)"
  
  echo "Breakdown of miscellaneous access by host (top 5):"
  grep "^http" $1 | grep -v whirled | grep -v $- |  awk '{print $1}' | \
      ruby -ruri -e 'ARGF.each do |url| begin puts URI.parse(url).host rescue Exception end end' | \
      sort | uniq -c | sort | tail -5
}

echo "ALL FILES for one day (October 1st 2010)":
echo "----------------------------------------";
foo oneday.txt

echo
echo "Only MP3 files for one month (October 2010)":
echo "-------------------------------------------";
foo mp3month.txt
