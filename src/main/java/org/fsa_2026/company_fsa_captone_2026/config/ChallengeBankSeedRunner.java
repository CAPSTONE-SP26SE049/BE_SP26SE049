package org.fsa_2026.company_fsa_captone_2026.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fsa_2026.company_fsa_captone_2026.entity.ChallengeBank;
import org.fsa_2026.company_fsa_captone_2026.entity.enums.SkillType;
import org.fsa_2026.company_fsa_captone_2026.repository.ChallengeBankRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChallengeBankSeedRunner implements CommandLineRunner {

    private final ChallengeBankRepository challengeBankRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("Checking if ChallengeBank needs seeding...");
        long count = challengeBankRepository.count();
        if (count == 0) {
            log.info("ChallengeBank is empty. Seeding default pronunciation challenges with IPA phonetic guides...");

            List<ChallengeBank> seeds = List.of(
                // BAC (North)
                ChallengeBank.builder()
                        .contentText("Lúa nếp là lúa nếp làng, lúa lên lớp lớp lòng nàng lâng lâng.")
                        .skillType(SkillType.SPEAKING)
                        .region("BAC")
                        .metadataJson(Map.of("ipaText", "luə˦˧ nep̚˧˥ laː˨˩ luə˦˧ nep̚˧˥ laːŋ˨˩, luə˦˧ len˧ lop̚˧˥ lop̚˧˥ laːŋ˨˩ naːŋ˨˩ ləŋ˧ ləŋ˧"))
                        .build(),
                ChallengeBank.builder()
                        .contentText("Hà Nội mùa này sương giăng lành lạnh, hoa sữa rơi đầy trên những con phố nhỏ.")
                        .skillType(SkillType.SPEAKING)
                        .region("BAC")
                        .metadataJson(Map.of("ipaText", "haː˨˩ noj˨˩˨ muə˨˩ naj˨˩ ʂɨəŋ˧ zaŋ˧ lajŋ˨˩ lajŋ˨˩˨, waː˧ ʂɨə˦˧ zəj˧ ɗaj˨˩ ʈen˧ ɲɨŋ˦˥ kɔn˧ fo˧˥ ɲɔ˧˩˨"))
                        .build(),
                ChallengeBank.builder()
                        .contentText("Con đường uốn lượn quanh sườn núi hiểm trở giữa làn mây trắng xóa.")
                        .skillType(SkillType.SPEAKING)
                        .region("BAC")
                        .metadataJson(Map.of("ipaText", "kɔn˧ ɗɨəŋ˨˩ uən˧˥ lɨəjŋ˨˩˨ kwaɲ˧ ʂɨəŋ˨˩ nuj˧˥ hiəm˧˩˨ ʈəː˧˩˨ zɨə˦˧ laːn˨˩ maj˧ ʈaŋ˧˥ swaː˧˥"))
                        .build(),
                ChallengeBank.builder()
                        .contentText("Trời nắng chang chang tiếng ve kêu râm ran khắp các ngả đường quê.")
                        .skillType(SkillType.SPEAKING)
                        .region("BAC")
                        .metadataJson(Map.of("ipaText", "ʈəj˨˩ naŋ˧˥ caːŋ˧ caːŋ˧ tieŋ˧˥ vɛ˧ kew˧ zəm˧ zaːn˧ xəp̚˧˥ kaːk̚˧˥ ɲaː˧˩˨ ɗɨəŋ˨˩ kwe˧"))
                        .build(),

                // TRUNG (Central)
                ChallengeBank.builder()
                        .contentText("Chiều chiều ra đứng ngõ sau, trông về quê mẹ ruột đau chín chiều.")
                        .skillType(SkillType.SPEAKING)
                        .region("TRUNG")
                        .metadataJson(Map.of("ipaText", "ciew˨˩ ciew˨˩ zaː˧ ɗɨŋ˧˥ ŋɔ˦˧ ʂaw˧, ʈoŋ˧ ve˨˩ kwe˧ mɛ˨˩˨ zwəjkt̚˧˥ ɗaw˧ cin˧˥ ciew˨˩"))
                        .build(),
                ChallengeBank.builder()
                        .contentText("Sông Hương nước chảy lờ lững trôi xuôi, mang theo tiếng chuông chùa Thiên Mụ ngân vang.")
                        .skillType(SkillType.SPEAKING)
                        .region("TRUNG")
                        .metadataJson(Map.of("ipaText", "ʂoŋ˧ hɨəŋ˧ nɨək̚˧˥ caːj˧˩˨ ləː˨˩ lɨŋ˦˧ ʈoj˧ suj˧, maːŋ˧ tʰɛw˧ tieŋ˧˥ cuəŋ˧ cuə˨˩ tʰien˧ mu˨˩˨ ŋən˧ vaːŋ˧"))
                        .build(),
                ChallengeBank.builder()
                        .contentText("Nắng miền Trung chói chang khô cằn, bóng dừa xanh che mát lối đi về.")
                        .skillType(SkillType.SPEAKING)
                        .region("TRUNG")
                        .metadataJson(Map.of("ipaText", "naŋ˧˥ mien˨˩ ʈuŋ˧ cɔj˧˥ caːŋ˧ xo˧ kən˨˩, bɔŋ˧˥ zɨə˨˩ ʂaɲ˧ cɛ˧ maːt̚˧˥ loj˧˥ ɗi˧ ve˨˩"))
                        .build(),
                ChallengeBank.builder()
                        .contentText("Tiếng chim hót lảnh lót đầu cành cây phượng vĩ đỏ rực góc sân trường.")
                        .skillType(SkillType.SPEAKING)
                        .region("TRUNG")
                        .metadataJson(Map.of("ipaText", "tieŋ˧˥ cim˧ hɔt̚˧˥ lajŋ˧˩˨ lɔt̚˧˥ ɗəw˨˩ kajŋ˨˩ kaj˧ fɨəjŋ˨˩˨ vi˦˧ ɗɔ˧˩˨ zɨk̚˨˩˨ ɣɔk̚˧˥ ʂən˧ ʈɨəŋ˨˩"))
                        .build(),

                // NAM (South)
                ChallengeBank.builder()
                        .contentText("Bánh mì Sài Gòn đặc ruột thơm ngon, hai ngàn một ổ nóng hổi vừa thổi vừa ăn.")
                        .skillType(SkillType.SPEAKING)
                        .region("NAM")
                        .metadataJson(Map.of("ipaText", "bajŋ˧˥ mi˨˩ ʂaːj˨˩ ɣɔn˨˩ ɗək̚˨˩ zwək̚˨˩ tʰəːm˧ ŋɔn˧, haːj˧ ŋaːn˨˩ mowt̚˨˩ ʔow˧˩˨ nawŋ˧˥ howj˧˩˨ vɨə˨˩ tʰowj˧˩˨ vɨə˨˩ ʔan˧"))
                        .build(),
                ChallengeBank.builder()
                        .contentText("Mùa nước nổi miền Tây mang theo phù sa bồi đắp cho những ruộng lúa mênh mông.")
                        .skillType(SkillType.SPEAKING)
                        .region("NAM")
                        .metadataJson(Map.of("ipaText", "muə˨˩ nɨək̚˧˥ noj˧˩˨ mien˨˩ taj˧ maːŋ˧ tʰɛw˧ fu˨˩ ʂaː˧ boj˨˩ ɗəp̚˧˥ cɔ˧ ɲɨŋ˦˥ zwəjŋ˨˩˨ luə˦˧ meɲ˧ moŋ˧"))
                        .build(),
                ChallengeBank.builder()
                        .contentText("Dưới bóng dừa mát rượi, chiếc ghe nhỏ từ từ lướt nhẹ trên dòng sông xanh.")
                        .skillType(SkillType.SPEAKING)
                        .region("NAM")
                        .metadataJson(Map.of("ipaText", "zɨəj˧˥ bɔŋ˧˥ zɨə˨˩ maːt̚˧˥ zɨəj˨˩˨, ciec̚˧˥ ɣɛ˧ ɲɔ˧˩˨ tɨ˨˩ tɨ˨˩ lɨət̚˧˥ ɲɛ˨˩˨ ʈen˧ zɔŋ˨˩ ʂoŋ˧ ʂaɲ˧"))
                        .build(),
                ChallengeBank.builder()
                        .contentText("Hôm nay trời nắng đẹp ghê, mình cùng đi chợ Bến Thành ăn chè nha.")
                        .skillType(SkillType.SPEAKING)
                        .region("NAM")
                        .metadataJson(Map.of("ipaText", "hom˧ naj˧ ʈəj˨˩ naŋ˧˥ ɗɛp̚˨˩˨ ɣe˧, miɲ˨˩ kuŋ˨˩ ɗi˧ cəː˨˩˨ ben˧˥ tʰaːjŋ˨˩ ʔan˧ cɛ˨˩ ɲaː˧"))
                        .build()
            );

            challengeBankRepository.saveAll(seeds);
            log.info("Successfully seeded {} default speaking challenges in ChallengeBank with IPA transcriptions!", seeds.size());
        } else {
            log.info("ChallengeBank already contains {} items. Skipping seeding.", count);
        }
    }
}
