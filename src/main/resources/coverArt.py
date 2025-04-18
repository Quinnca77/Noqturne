from ytmusicapi import YTMusic
import sys

ytmusic = YTMusic()
search_results = ytmusic.search(sys.argv[1], "songs")
videoId = ""
i = 0

for result in search_results:
    if i > 10:
        break
    videoId = result.get("videoId")
    print(videoId)
    i = i + 1