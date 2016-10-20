# KFGwifi
Simple app to automatize the logins to kfg wifi
If you don't know what the heck is this, it's probably not for you ;)

I made this project so I do not need to deal with the captive portal every time I use the WiFi in Karinthy.
You give the app your username & password, and it will login for you automatically, when your phone is connected to the WiFi.
It can keep alive the connection if you do not use it, and relogin, if necessary.
Sounds good, right?
Well, it doesn't always work... (I am going into the details below)
So, dear visitor if you do know what is this, and feel like helping, please try this app (available in Google Play),
and report any bugs to me! :) Thanks


Since each Android version handles wifi connectivity events (such as WIFI_STATE_CHANGED...) differently, it is a very hard work to 
make it work on all Android versions. Some versions send the "connected signal" before the phone is connected to the network!
A lot of different workarounds have to be made and some might not be compatible with the others, so that is why it is buggy sometimes.

