# Package Tracking Telegram Bot

This project is a Telegram bot that allows users to read a tracking code through QR code, barcode, or by entering it manually. Once the bot receives the tracking code, it searches for the most recent information available and presents it to the user to help them stay informed about their shipment.

The information is obtained through web scraping, which means that the bot automatically extracts data from websites to provide users with up-to-date information about their packages.

## Usage

To use this bot, simply start a chat with it on Telegram by clicking on the following link: [@mrw_tracking_bot](https://t.me/mrw_tracking_bot) . Once you have started a chat with the bot, provide your tracking code through QR code, barcode, or by entering it manually. The bot will then search for the most recent information available and present it to you in a clear and concise manner.

## Disclaimer

Please note that the information provided by this bot is obtained through web scraping and may not always be 100% accurate or up-to-date. Use this bot at your own risk.

## Local Installation

To install this bot locally, you will need to configure the following properties:

```
mrw.tracking.bot.username={{ bot name }}
mrw.tracking.bot.token={{ token }}
spring.data.mongodb.uri={{ mongodb connection }}
spring.data.mongodb.auto-index-creation=true // to generate indexes (optional)
```

Once you have configured these properties, you can start the bot by running the following command:

```
docker compose up
```

This will start the bot and connect it to the specified MongoDB database. You can then interact with the bot by starting a chat with it on Telegram.