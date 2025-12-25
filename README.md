## QuickMusicControls widget for Garmin Instinct 2 + Android companion app

I bought Garmin watch and they are great and all, but I was not happy with the music controls. 
The built-in music controls app requires extensive menu-diving to perform basic actions such as change volume or skip to a next track.
Furthermore, while listening to podcasts I grew used to specific behavior of the "Next track" button in Android Auto and other services - the button performs a "jump ahead 15 seconds" action instead of skip to the next podcast.

Luckily Garmin allows users to create their own apps and widgets for the watch using ConnectIQ SDK. So I created a super simple widget that does exactly what I expect.

The watch widget (written in Monkey C) provides a simple UI and sends commands to a companion Android app (written in Kotlin).
The companion app uses Android's media controller to relay the commands to whichever music player is currently active. And as a bonus, it recognizes if a podcast is playing (longer than 15 minutes, supports seek action)
and changes the "next track" behavior accordingly (skips 15 seconds forward or back).

<img width="235" height="311" alt="image" src="https://github.com/user-attachments/assets/611ba68e-7d07-4839-8f7e-e3e45c700094" />
<img width="235" height="315" alt="image" src="https://github.com/user-attachments/assets/48151871-162f-46b3-b194-fd173d05e1b0" />
