package tr.kontas.splitr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import tr.kontas.splitr.dto.base.BaseResponse;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CommandResponse extends BaseResponse {
    public CommandResponse(String id, String result) {
        super(id, result);
    }
}