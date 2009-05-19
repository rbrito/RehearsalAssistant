/*
 *  Author:
 *      Stjepan Rajko
 *      urbanSTEW
 *
 *  Copyright 2009 urbanSTEW.
 *
 *  This file is part of the openFrameworks Rehearsal Assistant.
 *
 *  openFrameworks Rehearsal Assistant is free software: you can redistribute it
 *  and/or modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 3 of the License,
 *  or (at your option) any later version.
 *
 *  openFrameworks Rehearsal Assistant is distributed in the hope that it will be
 *  useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with openFrameworks Rehearsal Assistant.
 *  If not, see <http://www.gnu.org/licenses/>.
 */
 
 #include "testApp.h"

//--------------------------------------------------------------
void testApp::setup(){	 
	
	// record 320x240 for now. 
	camWidth 		= 320;
	camHeight 		= 240;
	
	// initialize video grabber
	vidGrabber.setVerbose(true);
	vidGrabber.initGrabber(camWidth,camHeight);
	
	// allocate texture for the playback
	videoTexture.allocate(camWidth,camHeight, GL_RGB);
	
	// setup recording / playback state
	isRecording = false;
	isPlaying = false;
	playbackFrame = 0;
	
	// open an outgoing connection to HOST:PORT
	sender.setup( HOST, PORT );
	// and an incoming connection to PORT
	receiver.setup( PORT );
}


//--------------------------------------------------------------
// this gets called continuously
void testApp::update()
{
	// draw grey background
	ofBackground(100,100,100);
	
	// grab the next frame
	vidGrabber.grabFrame();
	
	// process the frame if it is new and we are recording
	if (vidGrabber.isFrameNew() && isRecording)
	{
		// push a new frame in the videoRecording vector
		videoRecording.push_back
		(
			videoFrame
			(
				new unsigned char [camWidth * camHeight * 3],
				ofGetElapsedTimef() - recordingStartTime
			)
		);

		// copy the grabbed frame pixels to the new frame
		int totalPixels = camWidth*camHeight*3;
		unsigned char * pixels = vidGrabber.getPixels();
		
		memcpy(videoRecording.back().frameData, pixels, totalPixels);
	}

	// check whether we are playing (also check vidGrabber.isFrameNew()
	// to make sure the playback rate matches the recording rate)
	if(vidGrabber.isFrameNew() && isPlaying)
	{
		// go to the next playback frame if possible
		if(playbackFrame + 1 < videoRecording.size())
			++playbackFrame;
		// if we have reached the end of the playback section, stop
		if(videoRecording[playbackFrame].timeStamp >= playbackStopTime)
			isPlaying = false;
	}
	
	// check for waiting messages
	while( receiver.hasWaitingMessages() )
	{
		// get the next message
		ofxOscMessage m;
		receiver.getNextMessage( &m );

		// check for session started message
		if ( m.getAddress() == "/RehearsalAssistant/sessionStarted" )
		{
			// session started means we need to start recording
			isRecording = true;
			// erase the existing contents of the vector
			for(int i=0; i<videoRecording.size(); i++)
				delete[] videoRecording[i].frameData;
			videoRecording.clear();
			// remember the time we started recording
			recordingStartTime = ofGetElapsedTimef();
		}

		// check for session stopped message
		if ( m.getAddress() == "/RehearsalAssistant/sessionStopped" )
			// session stopped means we should stop recording
			isRecording = false;

		// check for playback started message
		if ( m.getAddress() == "/RehearsalAssistant/playbackStarted" )
		{
			// playback started means we should play back the appropriate
			// segment of the recording
			isPlaying = true;
			// get the start and stop times from the message
			float start_time = m.getArgAsFloat( 0 );
			playbackStopTime = m.getArgAsFloat( 1 );

			// find the frame where we should stop playback
			for(playbackFrame=0; playbackFrame<videoRecording.size(); playbackFrame++)
				if(videoRecording[playbackFrame].timeStamp >= start_time)
					break;
		}
		
	}
}

//--------------------------------------------------------------
void testApp::draw()
{
	// white
	ofSetColor(0xffffff);
	// draw the camera input at 20,20
	vidGrabber.draw(20,20);
	
	// check whether we should display the playback frame
	if(videoRecording.size() && isPlaying)
	{
		videoTexture.loadData(videoRecording[playbackFrame].frameData, camWidth,camHeight, GL_RGB);
		videoTexture.draw(20+camWidth,20,camWidth,camHeight);
	}
	
	// black
	ofSetColor(0x000000);
	// display the current recording / playback frame
	ofDrawBitmapString("frame recorded: " + ofToString((int)videoRecording.size()),20,320);
	ofDrawBitmapString("frame played: " + ofToString(playbackFrame),20,340);
}


//--------------------------------------------------------------
void testApp::keyPressed  (int key)
{ 
	// invoke video grabber settings
	if (key == 's' || key == 'S')
		vidGrabber.videoSettings();

	// some control messages we can send to this app for debugging
	
	if (key == ' ')
	{
		// toggle recording
		ofxOscMessage m;
		m.setAddress(!isRecording ? "/RehearsalAssistant/sessionStarted" : "/RehearsalAssistant/sessionStopped");
		sender.sendMessage( m );
	}
	if (key == 'p' || key == 'P')
	{
		// request playback between 5.0s and 10.0s
		ofxOscMessage m;
		m.setAddress( "/RehearsalAssistant/playbackStarted" );
		m.addFloatArg( 5.0f );
		m.addFloatArg( 10.0f );
		sender.sendMessage( m );
	}
}


//--------------------------------------------------------------
void testApp::keyReleased(int key){ 
	
}

//--------------------------------------------------------------
void testApp::mouseMoved(int x, int y ){
	
}

//--------------------------------------------------------------
void testApp::mouseDragged(int x, int y, int button){
	
}

//--------------------------------------------------------------
void testApp::mousePressed(int x, int y, int button){
	
}

//--------------------------------------------------------------
void testApp::mouseReleased(int x, int y, int button){

}

//--------------------------------------------------------------
void testApp::windowResized(int w, int h){

}
