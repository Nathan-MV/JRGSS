require 'java'
module Audio
    def Audio.bgm_play(filename, volume = 100, pitch = 100 , pos)
      JAudio.bgm_play(filename, volume, pitch, pos)
    end
    
    def Audio.bgm_stop
      JAudio.bgm_stop()
    end

    def Audio.bgm_fade(time)
      JAudio.bgm_fade(time)
    end
  
    def Audio.bgs_play(filename, volume = 100, pitch = 100, pos)
      JAudio.bgs_play(filename, volume, pitch, pos)
    end
    def Audio.bgs_pos
        JAudio.bgs_pos()
    end
    def Audio.bgm_pos
        JAudio.bgs_pos()
    end

    def Audio.bgs_stop
      JAudio.bgs_stop()
    end
  
    def Audio.bgs_fade(time)
      JAudio.bgs_fade(time)
    end
  
    def Audio.me_play(filename, volume = 100, pitch = 100)
      JAudio.me_play(filename, volume, pitch)
    end
  
    def Audio.me_stop
      JAudio.me_stop()
    end
  
    def Audio.me_fade(time)
      JAudio.me_fade(time)
    end
  
    def Audio.se_play(filename, volume = 100, pitch = 100)
      JAudio.se_play(filename, volume, pitch)
    end
  
    def Audio.se_stop
      JAudio.se_stop()
    end
  end