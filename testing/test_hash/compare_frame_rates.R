#-set frame_size {frame_size}
command <- './fpcalc -raw -length 10000000 %s | sed -n \'s/\\(FINGERPRINT=\\)\\(.*\\)/\\2/p\''

frame_size <- 256 

full_file <- "originals/Ghosting.flac"
snip_file <- "recordings/ghosting1.flac"

for (i in 0:0) {
  npower <- 2^i

  
  full_command = sprintf(command, full_file)

  snip_command = sprintf(command, snip_file)
  
  full_res <- system(full_command, intern=TRUE)
  snip_res <- system(snip_command, intern=TRUE)
  
  full_res_p <- paste(full_res)
  snip_res_p <- paste(snip_res)
  
  print(full_res_p)
  print(snip_res_p)
}




sm <- read.table(snip, sep=',', as.is = TRUE)
gh <- read.table(full, sep=',', as.is = TRUE)
mat <- match(gh,sm)
mat
titl <- paste("Match:", as.character((sum( !is.na(mat) ) / length(sm))*100), "%")
titl
length(sm)
plot(match(gh,sm), xlab=paste("Song1: ",full), ylab=paste("Snippet: ", snip), main=titl);
