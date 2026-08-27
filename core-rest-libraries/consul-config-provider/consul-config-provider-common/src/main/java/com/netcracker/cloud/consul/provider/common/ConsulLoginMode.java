package com.netcracker.cloud.consul.provider.common;

//todo vlla поступил комментарий от автора требований не использовать имя AUTO, а сделать более явным, использовать что-то со словом FALLBACK например. Тут и в соответствующей проперте
public enum ConsulLoginMode {
    AUTO,
    KUBERNETES,
    M2M //todo vlla я что то не нашел места, где у нас проставляется мод m2m. Мы точно ввели и читаем новую проперти для этого?
}
