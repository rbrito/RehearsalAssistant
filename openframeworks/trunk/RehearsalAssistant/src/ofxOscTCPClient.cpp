/*

 Copyright 2009 Stjepan Rajko
 Copyright 2007, 2008 Damian Stewart damian@frey.co.nz
 Distributed under the terms of the GNU Lesser General Public License v3

 This file is part of the ofxOsc openFrameworks OSC addon.

 ofxOsc is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 ofxOsc is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with ofxOsc.  If not, see <http://www.gnu.org/licenses/>.
 */

#include "ofxOscTCPClient.h"

#include "OscReceivedElements.h"
#include <UdpSocket.h>

#include <Poco/Net/NetException.h>

#include <iostream>
#include <assert.h>

ofxOscTCPClient::ofxOscTCPClient()
{

}

void ofxOscTCPClient::setup( const Poco::Net::SocketAddress &address )
{
    try
    {
        socket.connect(address);
        thread.start(&ofxOscTCPClient::socketListener, this);
	} catch(Poco::Net::ConnectionRefusedException &)
	{
	}
}

ofxOscTCPClient::~ofxOscTCPClient()
{
    // delete messages in queue
    for(std::deque< ofxOscMessage* >::iterator it=messages.begin(); it!=messages.end(); it++)
        delete *it;
}

void ofxOscTCPClient::socketListener(void *thisClient)
{
    ofxOscTCPClient *This = static_cast<ofxOscTCPClient *> (thisClient);
    
    for(;;)
    {
        int length = This->socket.receiveBytes(This->buffer, 1024);
        if(!length)
            break;
        This->ProcessPacket(This->buffer, length, IpEndpointName());
    }
}

void ofxOscTCPClient::ProcessMessage( const osc::ReceivedMessage &m, const IpEndpointName& remoteEndpoint )
{
	// convert the message to an ofxOscMessage
	ofxOscMessage* ofMessage = new ofxOscMessage();

	// set the address
	ofMessage->setAddress( m.AddressPattern() );

	// set the sender ip/host
	char endpoint_host[ IpEndpointName::ADDRESS_STRING_LENGTH ];
	remoteEndpoint.AddressAsString( endpoint_host );
    ofMessage->setRemoteEndpoint( endpoint_host, remoteEndpoint.port );

	// transfer the arguments
	for ( osc::ReceivedMessage::const_iterator arg = m.ArgumentsBegin();
		  arg != m.ArgumentsEnd();
		  ++arg )
	{
		if ( arg->IsInt32() )
			ofMessage->addIntArg( arg->AsInt32Unchecked() );
		else if ( arg->IsFloat() )
			ofMessage->addFloatArg( arg->AsFloatUnchecked() );
		else if ( arg->IsString() )
			ofMessage->addStringArg( arg->AsStringUnchecked() );
		else
		{
			assert( false && "message argument is not int, float, or string" );
		}
	}

	// now add to the queue

	// at this point we are running inside the thread created by startThread,
	// so anyone who calls hasWaitingMessages() or getNextMessage() is coming
	// from a different thread

	// so we have to practise shared memory management

	// grab a lock on the queue
    Poco::Mutex::ScopedLock lock(mutex);
    
	// add incoming message on to the queue
	messages.push_back( ofMessage );

}

bool ofxOscTCPClient::hasWaitingMessages()
{
	// grab a lock on the queue
    Poco::Mutex::ScopedLock lock(mutex);

	// check the length of the queue
	// return whether we have any messages
	return static_cast<int>(messages.size()) > 0;
}

bool ofxOscTCPClient::getNextMessage( ofxOscMessage* message )
{
	// grab a lock on the queue
    Poco::Mutex::ScopedLock lock(mutex);

	// check if there are any to be got
	if ( messages.size() == 0 )
		return false;

	// copy the message from the queue to message
	ofxOscMessage* src_message = messages.front();
	message->copy( *src_message );

	// now delete the src message
	delete src_message;
	// and remove it from the queue
	messages.pop_front();

	// return success
	return true;
}
