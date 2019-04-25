import bluetooth_thread
import sensor
import time
import signal
import sys


class Main(sensor.Sensor):

    __BREAK_OFF = b'10'
    __BREAK_ON = b'11'

    __LED_OFF = b'20'
    __LED_ON = b'21'

    __LED_MANUAL = b'30'
    __LED_AUTO = b'31'

    def __init__(self):
        super().__init__()
        self.__bt = bluetooth_thread.Bluetooth_thread()
        self.__bt.set_connection_start_callback(
            callback=self.bluetooth_connection_callback)
        self.__bt.set_connection_stop_callback(
            callback=self.bluetooth_disconnection_callback)
        self.__bt.start()

        self.__break_state = self.__BREAK_OFF
        self.__led_state = self.__LED_OFF
        self.__led_auto_state = self.__LED_MANUAL

        signal.signal(signal.SIGINT, self.signal_handler)
        self.__old_pir = 0

    def start(self):
        self.__run()

    def __run(self):
        while True:
            # 블루투스 데이터 수신
            data = self.__bt.recv()
            if data is not None:
                if data in [self.__BREAK_OFF, self.__BREAK_ON]:
                    self.__break_state = data

                elif data in [self.__LED_OFF, self.__LED_ON]:
                    if self.__led_auto_state == self.__LED_MANUAL:
                        self.__led_state = data

                elif data in [self.__LED_MANUAL, self.__LED_AUTO]:
                    self.__led_auto_state = data

            pir = self.pir_read()
            if pir == 0 and self.__old_pir == 1:
                # 움직이는 것으로 인식된 물체가 없으면 브레이크
                self.__break_state = self.__BREAK_ON
                try:
                    self.__bt.send(self.__break_state)

                except:
                    pass

            self.__old_pir = pir

            # 조도 센서에 따라 LED 제어하는 경우
            # if self.__led_auto_state == self.__LED_AUTO:
            #     illum = self.illum_read()
            #     if illum == 0:
            #         self.__led_state == self.__LED_ON

            #     else:
            #        self.__led_state == self.__LED_OFF

            #     try:
            #         self.__bt.send(self.__led_state)

            #     except:
            #         pass

            # if self.__led_state == self.__LED_ON:
            #     self.led_on()
            # else:
            #     self.led_off()

            if self.__break_state == self.__BREAK_ON:
                self.break_on()
            else:
                self.break_off()

            time.sleep(0.2)

        self.__bt.close()

    def bluetooth_connection_callback(self):
        # 블루투스 연결되면 현재 상태 전송
        self.__bt.send(self.__break_state + b'\r')
        self.__bt.send(self.__led_auto_state + b'\r')
        self.__bt.send(self.__led_state + b'\r')

    def bluetooth_disconnection_callback(self):
        # 블루투스 끊기면 브레이크
        self.__break_state = self.__BREAK_OFF

    def signal_handler(self, sig, frame):
        self.__bt.close()
        sys.exit()


if __name__ == '__main__':
    m = Main()
    m.start()
