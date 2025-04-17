# Como usar este script de testes

Primeiro, compile dentro do Arduino Uno (usando o Arduino IDE por exemplo) o script `arduino_random_data.ino`.

Certifique-se de que está usando um ambiente virtual (venv) para rodar o python. Você pode criar/ativar um ambiente usando o comando na raíz do repositório:

```bash
python3 -m venv .venv
source .venv/bin/activate
```

Você agora deverá ter um shell marcando (.venv) no início.
Agora você pode instalar as dependências do script `receive_test.py` usando:

```bash
# Entre na pasta de teste de dados aleatórios 
cd referencias/arduino_random_data
pip install -r requirements.txt
```

Agora rode o script usando:

```bash
python3 receive_test.py
```
