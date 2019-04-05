# SeedPlanter
Makes it faster to replant seeds in farms. Useful on servers where farming is a big and important part of the economy. 

## How to use
Plant one seed, then right-click the placed seed. If you have the permission node `seedplanter.plant` and more of the planted seed in your hand the script will automatically plant the seeds in your hand for you. (Assuming there is adjacent empty farmlands and you have permission to build in the area.) For every seed planted, one seed will be removed from your hand. If you switch slot or run out of seeds in your hand then the auto-planting will stop. 

## Config.yml
* `maxSpread` - The maximum radius to auto-plant seeds out from the origin seed the player first right-clicked.

* `maxPlant` - The maximum number of seeds to auto-plant.

* `tickDelay` - The number of ticks to delay between auto-planting seeds. If `tickDelay <= 0` then the delay is set to 1. If `tickDelay < 1` then the delay is set to 1 and `1 / tickDelay` number of seeds will be planted every tick. (E.g. `tickDelay = 0.2` will plant 5 seeds per tick, and 100 seeds per second.)

## Issues
* Anti-hack plugins may complain and cancel placing of seeds if the config is set to place too many seeds at too high of a rate.