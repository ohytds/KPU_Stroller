import bluetooth
import threading
import queue


class Bluetooth_thread(threading.Thread):
    def __init__(self):
        super().__init__()
        self.__host = ''
        self.__port = bluetooth.PORT_ANY
        self.__uuid = "00000001-0000-1000-8000-00805F9B34FB"

        # 수신 데이터 저장용 큐
        self.__recv_queue = queue.Queue()
        self.daemon = True

        # 블루투스 서버 소켓 생성
        self.__socket = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
        self.__socket.bind((self.__host, self.__port))
        self.__socket.listen(1)
        bluetooth.advertise_service(self.__socket, name="bluetooth_server",
                                    service_id=self.__uuid,
                                    service_classes=[
                                        self.__uuid, bluetooth.SERIAL_PORT_CLASS],
                                    profiles=[bluetooth.SERIAL_PORT_PROFILE])

        self.__connection_start_callback = None
        self.__connection_stop_callback = None

        self.__is_connected = False

    def run(self):
        while True:
            # 블루투스 연결 대기
            self.__connected_socket, client_address = self.__socket.accept()
            self.__is_connected = True

            self.__start_callback()

            try:
                while True:
                    # 데이터를 받는데 오류가 생기면 블루투스가 끊긴 것으로 판단
                    data = self.__connected_socket.recv(1024)
                    self.__recv_queue.put(data)

            except:
                pass

            self.__stop_callback()

            self.__is_connected = False
            self.__connected_socket.close()

    def close(self):
        self.__connected_socket.close()
        self.__socket.close()

    # 수신 데이터
    def recv(self):
        try:
            data = self.__recv_queue.get(block=False)

        except queue.Empty:
            return None

        return data

    # 송신 데이터
    def send(self, data):
        if self.__is_connected:
            self.__connected_socket.send(data)

    # 연결 시작 시 호출되는 콜백 설정
    def set_connection_start_callback(self, callback=None, args=()):
        self.__connection_start_callback = callback
        self.__connection_start_callback_args = args

    def __start_callback(self):
        if self.__connection_start_callback is not None:
            self.__connection_start_callback(
                *self.__connection_start_callback_args)

    # 연결 종료 시 호출되는 콜백 설정
    def set_connection_stop_callback(self, callback=None, args=()):
        self.__connection_stop_callback = callback
        self.__connection_stop_callback_args = args

    def __stop_callback(self):
        if self.__connection_stop_callback is not None:
            self.__connection_stop_callback(
                *self.__connection_stop_callback_args)

    def is_connected(self):
        return self.__is_connected
