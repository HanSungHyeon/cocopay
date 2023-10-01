import React from "react";
import { Container } from "@/components/atoms/Container/Container.styles";
import Background4 from "@/assets/images/bg-onboarding4.png";
import { Image } from "@/components/atoms/Image/Image";
import Background4_2 from "@/assets/images/bg-onboarding4-2.png";
import Peng from "@/assets/images/img-peng-onboarding4.png";
import { Wrapper } from "@/components/atoms/Wrapper/Wrapper.styles";
import { OnboardingText } from "@/pages/OnboardingPage/OnboardingPage.styles";
import styled, { keyframes } from "styled-components";
import handcard from "@/assets/images/img-hancard.png";

// onNextPage의 타입을 명시적으로 정의
type OnboardingPage4Props = {
  onNextPage: () => void; // 예를 들어, 이동 함수의 타입은 void로 정의할 수 있습니다.
};

const moveTop = keyframes`
  from{
    transform: translateY(0);
  }
  to {
    transform: translateY(0);
  }
`;

const AnimatedImage = styled(Image)`
  position: absolute;
  bottom: 0;
  left: 3%;
  top: 41%;
  transform: translateY(-50%);
  animation: ${moveTop} 3s forwards;
`;

function OnboardingPage4(props: OnboardingPage4Props) {
  const { onNextPage } = props;
  const handlePageTransition = () => {
    // 페이지 이동 로직 추가
    onNextPage(); // 다음 페이지로 이동
  };

  //TODO: username API로부터 받기
  const Username = { name: "성현" };
  return (
    // TODO: 반응형 크기 맞추기
    <Container
      style={{
        backgroundImage: `url(${Background4_2})`,
        backgroundSize: "cover",
        backgroundRepeat: "no-repeat",
        backgroundPosition: "center center",
      }}
      onClick={handlePageTransition}
    >
      <Container $paddingTop="100px">
        <Wrapper $alignItems="start">
          <OnboardingText>이제부터는 코코가</OnboardingText>
          <OnboardingText>{Username.name}님의</OnboardingText>
          <OnboardingText>슬기로운 소비생활을</OnboardingText>
          <OnboardingText>도와드릴게요!</OnboardingText>
        </Wrapper>
        <AnimatedImage
            src={handcard}
            width={17} />
        <Container>
          <Image
              src={Peng}
              width={27}
          style={{
            position:"absolute",
            left: "40%",
            bottom: "7%"
          }}/>
        </Container>
      </Container>
    </Container>
  );
}

export default OnboardingPage4;
