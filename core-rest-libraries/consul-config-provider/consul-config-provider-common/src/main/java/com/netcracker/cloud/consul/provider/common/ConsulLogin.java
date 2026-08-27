package com.netcracker.cloud.consul.provider.common;

import java.io.IOException;

//todo vlla мне не очень нравится нейминг этого класса и метода perform, а так же связка классов ProbingConsulLogin, ConsulLogin и TokenProvider. ProbingConsulLogin одновременно и наследник ConsulLogin, и аггрегирует в себе несколько других ConsulLogin. Думаю, мы тут можем подумать, какой паттенр лучше всего подходимт под эту ситуацию и можем сделать некоторый рефакторинг. Сначала проанализируй и предложи варианты, не исправляй сразу.
//возможно, достаточно просто переименовать ConsulLogin в ConsulTokenProvider, а сам старый TokenProvider во что-то еще
public interface ConsulLogin {

    Token perform() throws IOException;
}
