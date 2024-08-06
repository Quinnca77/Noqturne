from ytmusicapi import YTMusic
import sys

def main33():
    ytmusic = YTMusic()
    search_results = ytmusic.search(sys.argv[2], "songs")
    videoId = ""
    i = 0
    # TODO if no decent match is found (hamming distance song title), return special value and get artist picture instead
    for result in search_results:
        if i > 10:
            break
        videoId = result.get("videoId")
        print(videoId)
        i = i + 1

if __name__ == '__main__':
    globals()[sys.argv[1]]()
    sys.exit(0)