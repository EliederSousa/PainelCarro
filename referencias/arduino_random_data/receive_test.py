import serial, time, json, pygame

pygame.init()

janela = pygame.display.set_mode((400, 300))
cor_fundo = (255, 255, 255)
janela.fill(cor_fundo)
pygame.display.update()
font = pygame.font.SysFont(None, 32)

# def sendToArduino(sendStr):
#    ser.write(sendStr.encode('utf-8')) # change for Python3

def recvFromArduino():
    global startMarker, endMarker
    
    ck = ""
    x = "z" # any value that is not an end- or startMarker
    byteCount = -1 # to allow for the fact that the last increment will be one too many
    
    # wait for the start character
    while ord(x) != startMarker: 
        x = ser.read()
    
    # save data until the end marker is found
    while ord(x) != endMarker:
        if ord(x) != startMarker:
            ck = ck + x.decode("utf-8") # change for Python3
            byteCount += 1
        x = ser.read()
    
    return(ck)


#============================

def readArduino():

    # wait until the Arduino sends 'Arduino Ready' - allows time for Arduino reset
    # it also ensures that any bytes left over from a previous message are discarded
    
    global startMarker, endMarker, janela, font
    msg = ""
    while msg.find("STOPIT") == -1:

        while ser.inWaiting() == 0:
            pass
        
        msg = recvFromArduino()
        h = json.loads(msg)
        janela.fill(pygame.Color(255,255,255))
        velocityText = font.render(str(h["vel"])+"km/h", False, pygame.Color(0, 0, 0))
        janela.blit(velocityText, (20, 20))
        velocityText = font.render(str(h["amp"])+"A", False, pygame.Color(0, 0, 0))
        janela.blit(velocityText, (20, 70))
        velocityText = font.render(str(h["volt"])+"V", False, pygame.Color(0, 0, 0))
        janela.blit(velocityText, (20, 120))
        velocityText = font.render(str( h["volt"] * h["amp"] )+"W", False, pygame.Color(0, 0, 0))
        janela.blit(velocityText, (20, 170))
        pygame.display.update()

        for evento in pygame.event.get():
            if evento.type == pygame.QUIT:
                pygame.quit()
                exit()
        

# Use COMX caso esteja no Windows. Verifique a porta no Gerenciador de Dispositivos.
# serPort = "COM3"

# Use /dev/ttyX caso esteja no Linux. Use dmesg para descobrir onde o Arduino foi montado.
serPort = "/dev/ttyACM0"
baudRate = 115200

ser = serial.Serial(serPort, baudRate)

startMarker = 60
endMarker = 62

readArduino()
ser.close
