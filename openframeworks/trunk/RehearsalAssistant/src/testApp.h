#ifndef _TEST_APP
#define _TEST_APP


#include "ofMain.h"

#include "ofxOsc.h"

#include <Poco/SharedPtr.h>

#define HOST "localhost"
#define PORT 12345

struct videoFrame
{
	videoFrame(unsigned char * data, float time)
		: frameData(data), timeStamp(time)
	{}

	unsigned char * frameData;
	float timeStamp;
};

class testApp : public ofBaseApp{
	
	public:
		
		void setup();
		void update();
		void draw();
		
		void keyPressed(int key);
		void keyReleased(int key);
		void mouseMoved(int x, int y );
		void mouseDragged(int x, int y, int button);
		void mousePressed(int x, int y, int button);
		void mouseReleased(int x, int y, int button);
		void windowResized(int w, int h);

private:		
		ofVideoGrabber 		vidGrabber;
		std::vector<videoFrame> 	videoRecording;
		ofTexture			videoTexture;
		int 				camWidth;
		int 				camHeight;

		bool				isRecording;
		float				recordingStartTime;


		bool				isPlaying;
		int					playbackFrame;
		float				playbackStopTime;
		
		ofxOscReceiver		receiver;
		ofxOscSender		sender;
};

#endif	
