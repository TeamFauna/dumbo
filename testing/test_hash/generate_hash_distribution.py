import os
import csv

#-set frame_size {frame_size}
command = """
./fpcalc  -raw -length 10000000 {src_file} | sed -n 's/\(FINGERPRINT=\)\(.*\)/\\2/p' > {dest_file}
""" 

frame_size = 256

full_file = "originals/Ghosting.flac"
snip_file = "recordings/ghosting1.flac"

for i in range(1):
  npower = 2**i

  full_command = command.format(
      #frame_size = npower*frame_size,
      src_file = full_file,
      dest_file = str(npower*frame_size) + "full.hash")

  snip_command = command.format(
      #frame_size = npower,
      src_file = snip_file,
      dest_file = str(npower*frame_size) + "snip.hash")

  print(full_command)
  print(snip_command)

  os.system(full_command)
  os.system(snip_command)



  
