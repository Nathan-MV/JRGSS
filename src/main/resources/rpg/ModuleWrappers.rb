require 'java'
module Audio
  #==== BGM
  def self.bgm_play(filename, volume = 100, pitch = 100 , pos = 0)
    JAudio.bgm_play(filename, volume, pitch, pos)
  end
  def self.bgm_stop
    JAudio.bgm_stop()
  end
  def self.bgm_fade(time)
    JAudio.bgm_fade(time)
  end
  def self.bgm_pos
      JAudio.bgs_pos()
  end

  #==== BGS
  def self.bgs_play(filename, volume = 100, pitch = 100, pos = 0)
    JAudio.bgs_play(filename, volume, pitch, pos)
  end
  def self.bgs_pos
      JAudio.bgs_pos()
  end
  def self.bgs_stop
    JAudio.bgs_stop()
  end
  def self.bgs_fade(time)
    JAudio.bgs_fade(time)
  end

  #==== ME
  def self.me_play(filename, volume = 100, pitch = 100)
    JAudio.me_play(filename, volume, pitch)
  end
  def self.me_stop
    JAudio.me_stop()
  end
  def self.me_fade(time)
    JAudio.me_fade(time)
  end

  #==== SE
  def self.se_play(filename, volume = 100, pitch = 100)
    JAudio.se_play(filename, volume, pitch)
  end

  def self.se_stop
    JAudio.se_stop()
  end
end 