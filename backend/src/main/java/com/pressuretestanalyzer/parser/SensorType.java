package com.pressuretestanalyzer.parser;

/**
 * Fabricante/modelo do sensor que gerou o arquivo enviado pela API. Hoje
 * apenas documenta e valida a entrada da requisicao: a escolha do
 * {@link SensorFileParser} continua sendo estrutural, via
 * {@link SensorFileParserResolver}, para nao duplicar essa logica aqui.
 * Quando um segundo fabricante for suportado, este enum pode passar a guiar
 * a selecao do parser se a deteccao estrutural nao for mais suficiente.
 */
public enum SensorType {
    TEKSENSOR
}
