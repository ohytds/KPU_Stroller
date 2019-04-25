import wiringpi


class Sensor():
    __MOTOR_PIN = 18
    __PIR_PIN = 22

    # 디지털 아웃 조도 센서 모듈
    # __ILLUM_PIN =
    # 조명, 전압이 다르거나 전류소모가 크면 릴레이모듈 사용
    # __LED_PIN =

    def __init__(self):
        # BCM 핀 번호 사용
        wiringpi.wiringPiSetupGpio()

        # 모터 설정
        wiringpi.pinMode(self.__MOTOR_PIN, wiringpi.PWM_OUTPUT)
        wiringpi.pwmSetMode(wiringpi.PWM_MODE_MS)
        # 19200000/38400/100 == 50 Hz
        wiringpi.pwmSetRange(100)
        wiringpi.pwmSetClock(3840)
        wiringpi.pwmWrite(self.__MOTOR_PIN, 0)

        # PIR input 설정
        wiringpi.pinMode(self.__PIR_PIN, wiringpi.INPUT)

        # # 조도 input 설정
        # wiringpi.pinMode(self.__ILLUM_PIN, wiringpi.INPUT)

        # # LED output 설정
        # wiringpi.pinMode(self.__LED_PIN, wiringpi.OUTPUT)

    def break_on(self):
        wiringpi.pwmWrite(self.__MOTOR_PIN, 50)

    def break_off(self):
        wiringpi.pwmWrite(self.__MOTOR_PIN, 0)

    def pir_read(self):
        return wiringpi.digitalRead(self.__PIR_PIN)

    # def illum_read(self):
    #     return wiringpi.digitalRead(self.__ILLUM_PIN)

    # def led_on(self):
    #     wiringpi.digitalWrite(self.__LED_PIN, wiringpi.HIGH)

    # def led_off(self):
    #     wiringpi.digitalWrite(self.__LED_PIN, wiringpi.LOW)
