#-set frame_size {frame_size}
command <- 'fpcalc -raw -length 10000000 -set frame_size=%i %s | sed -n \'s/\\(FINGERPRINT=\\)\\(.*\\)/\\2/p\''

frame_size <- 128

full_file <- "Ghosting.flac"
snip_file <- "ghosting1.flac"

for (i in 0:0) {
  npower <- 2^i
  
  
  full_command = sprintf(command, frame_size, full_file)
  
  snip_command = sprintf(command, frame_size, snip_file)
  
  #print(full_command)
  #print(snip_command)
  
  full_res <- system(full_command, intern=TRUE)
  snip_res <- system(snip_command, intern=TRUE)
  
  full_res_p <- paste(full_res,collapse="")
  snip_res_p <- paste(snip_res, collapse="")
  
  #print(full_res_p)
  #print(snip_res_p)
  
  full_a <- strsplit(full_res_p, ",")[[1]]
  snip_a <- strsplit(snip_res_p, ",")[[1]]
  
  mat <- match(full_a, snip_a)
  mat
  
  titl <- paste("Match:", as.character((sum( !is.na(mat) ) / length(snip_a))*100), "%, at: ", as.character(frame_size))
  plot(mat, xlab=paste("Song1: ",full_file), ylab=paste("Snippet: ", snip_file), main=titl);
}




#sm <- read.table(snip, sep=',', as.is = TRUE)
#gh <- read.table(full, sep=',', as.is = TRUE)
#mat <- match(gh,sm)
#mat
#titl <- paste("Match:", as.character((sum( !is.na(mat) ) / length(sm))*100), "%")
#titl
#length(sm)
#plot(match(gh,sm), xlab=paste("Song1: ",full), ylab=paste("Snippet: ", snip), main=titl);