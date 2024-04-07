from ytmusicapi import YTMusic
import sys

def main33():
    ytmusic = YTMusic()
    search_results = ytmusic.search(sys.argv[2])
    videoId = ""
    for result in search_results:
        if result.get("resultType") == "song":
            videoId = result.get("videoId")
            break
    print(videoId)

if __name__ == '__main__':
    globals()[sys.argv[1]]()
    #should be 0 for successful exit
    #however just to demostrate that this value will reach Java in exit code
    sys.exit(220)