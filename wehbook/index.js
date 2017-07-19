// Copyright 2016, Google, Inc.
// Licensed under the Apache License, Version 2.0 (the 'License');
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an 'AS IS' BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

'use strict';

process.env.DEBUG = 'actions-on-google:*';
const App = require('actions-on-google').ApiAiApp;
const functions = require('firebase-functions');

const FIND_ROOM = 'find.room';

const ROOM_ARGUMENT = 'ROOM_NAME';

// [START SillyNameMaker]
exports.sillyNameMaker = functions.https.onRequest((request, response) => {
  console.log('REQUEST HIT');
  console.log('REQUEST: ' + JSON.stringify(request.body));
  const app = new App({request, response});
  console.log('Request headers: ' + JSON.stringify(request.headers));
  console.log('Request body: ' + JSON.stringify(request.body));

  // Make a silly name
  function findRoom (app) {
    let room = app.getArgument(ROOM_ARGUMENT); 
    if (room === 'BATHROOM') {
      app.tell("Your bathroom is in the lobby downstairs");
    } else if (room === 'BEDROOM') {
      app.tell("Your bedroom is upstairs, at the end of the hall on the right");
    } else if (room === 'KITCHEN') {
      app.tell("Its near the front door to the right")
    } else {
      app.tell('Default response');
    }
    //app.tell('Alright, your silly name is ' +
    //  color + ' ' + number +
    //  '! I hope you like it. See you next time.');
  }

  let actionMap = new Map();
  actionMap.set(FIND_ROOM, findRoom);

  app.handleRequest(actionMap);
});
// [END SillyNameMaker]
