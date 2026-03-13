package cc.ginpika.yuni.core;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Data
@Slf4j
public class YuniSession {
    List<YuniMessage> messages = new ArrayList<>();

    public void reply(YuniReply msg) {

    }
}
